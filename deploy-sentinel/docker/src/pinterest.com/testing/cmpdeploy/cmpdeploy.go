package main

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net"
	"net/http"
	"os"
	"path"
	"strings"
	"time"

	"github.com/aws/aws-sdk-go/aws"
	"github.com/aws/aws-sdk-go/aws/ec2metadata"
	"github.com/aws/aws-sdk-go/aws/session"
	"github.com/aws/aws-sdk-go/service/ec2"
	"github.com/docker/docker/api/types"
	"github.com/docker/docker/client"
	"github.com/knadh/koanf"
	"github.com/knadh/koanf/parsers/yaml"
	"github.com/knadh/koanf/providers/file"
	"github.com/samuel/go-zookeeper/zk"
)

type HostData struct {
	IpAddress string   `json:"ipaddress"`
	ZkNodes   []string `json:"zknodes"`
	EnvVars   []string `json:"env_vars"`
}

type ServiceConf struct {
	prod        bool
	zkHost      string
	zkPath      string
	nodeName    string
	zumPath     string
	singerPath  string
	metricsPort string
	listenPort  string
}

func NewServiceConf() ServiceConf {
	var prod bool
	prodStr := os.Getenv("PROD")
	if prodStr == "" {
		prod = false
	} else {
		prod = true
	}
	zkHost := os.Getenv("ZK_CLUSTER")
	if zkHost == "" {
		zkHost = "127.0.0.1:2181"
	}
	zkPath := os.Getenv("ZK_PATH")
	if zkPath == "" {
		zkPath = "/discovery/cmpdocker"
	}
	nodeName := os.Getenv("EPHEMERAL_NAME")
	if nodeName == "" {
		nodeName = "testnode"
	}
	zumPath := os.Getenv("ZUM_PATH")
	if zumPath == "" {
		zumPath = "/var/serverset"
	}
	singerPath := os.Getenv("SINGER_PATH")
	if singerPath == "" {
		singerPath = "/var/log/deploy-sentinel"
	}
	metricsPort := os.Getenv("METRICS_PORT")
	if metricsPort == "" {
		metricsPort = "9999"
	}
	listenPort := os.Getenv("LISTEN_PORT")
	if listenPort == "" {
		listenPort = "8000"
	}
	return ServiceConf{prod, zkHost, zkPath, nodeName, zumPath, singerPath, metricsPort, listenPort}
}

func main() {
	verifyFactsAndUUID()
	verifySidecarContainers()

	conf := NewServiceConf()
	c, _, err := zk.Connect([]string{conf.zkHost}, time.Second)
	if err != nil {
		panic(err)
	}
	ephemeral := createEphemeral(c, conf.prod, conf.zkPath, conf.nodeName)

	http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintf(w, healthCheck(c, ephemeral, conf))
	})
	http.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		json.NewEncoder(w).Encode(HostData{
			IpAddress: getLocalIP(),
			ZkNodes:   getNodes(c),
			EnvVars:   os.Environ()})
	})
	log.Fatal(http.ListenAndServe(fmt.Sprintf(":%s", conf.listenPort), nil))
}

func createEphemeral(c *zk.Conn, prod bool, zkPath, nodeName string) string {
	var data []byte

	if !prod {
		_, err := createInternal(c, zkPath, data, zk.WorldACL(zk.PermAll), true)
		if err != nil {
			if err != zk.ErrNodeExists {
				panic(err)
			}
		}
	}

	result, err := c.CreateProtectedEphemeralSequential(path.Join(zkPath, nodeName), data, zk.WorldACL(zk.PermAll))
	if err != nil {
		panic(err)
	}
	fmt.Println("Create ephemeral:", result)
	return result
}

func createInternal(c *zk.Conn, zkPath string, data []byte, acl []zk.ACL, force bool) (string, error) {
	if zkPath == "/" {
		return "/", nil
	}

	fmt.Println("creating: %s", zkPath)
	attempts := 0
	for {
		attempts += 1
		returnValue, err := c.Create(zkPath, data, 0, acl)
		fmt.Println("create status for %s: %s, %+v", zkPath, returnValue, err)
		if err != nil && force && attempts < 2 {
			returnValue, err = createInternal(c, path.Dir(zkPath), []byte("cmpdocker auto-generated"), acl, force)
		} else {
			return returnValue, err
		}
	}
	return "", nil
}

func healthCheck(c *zk.Conn, ephemeral string, conf ServiceConf) string {
	var checks []string
	checks = append(checks, fmt.Sprintln("zk:", zkCheck(c, ephemeral, conf)))
	checks = append(checks, fmt.Sprintln("singer:", singerCheck(conf.singerPath)))
	checks = append(checks, fmt.Sprintln("zum:", zumCheck(conf.zumPath, conf.zkPath, conf.nodeName)))
	checks = append(checks, fmt.Sprintln("metrics:", metricsCheck(conf.metricsPort)))
	return strings.Join(checks, "")
}

func zkCheck(c *zk.Conn, ephemeral string, conf ServiceConf) bool {
	children, _, err := c.Children(conf.zkPath)
	if err != nil {
		panic(err)
	}
	for _, element := range children {
		if path.Join(conf.zkPath, element) == ephemeral {
			return true
		}
	}
	return false
}

func singerCheck(singerPath string) bool {
	filename := path.Join(singerPath, "test.log")
	file, err := os.Create(filename)
	if err != nil {
		fmt.Println("error creating", filename)
		fmt.Println(err)
		return false
	}
	numBytes, err := file.WriteString("testing logging")
	if err != nil {
		fmt.Println("error writing", filename)
		fmt.Println(err)
		return false
	}
	fmt.Sprintln("wrote %d bytes to %s", numBytes, filename)
	if err := file.Close(); err != nil {
		fmt.Println("error closing", filename)
		fmt.Println(err)
		return false
	}
	return true
}

func zumCheck(zumPath, zkPath, nodeName string) bool {
	serverset := strings.Replace(strings.TrimLeft(zkPath, "/"), "/", ".", -1)
	filename := path.Join(zumPath, serverset)
	contents, err := ioutil.ReadFile(filename)
	if err != nil {
		fmt.Println("error reading", filename)
		fmt.Println(err)
		return false
	}
	fmt.Println(string(contents))
	return true
}

func metricsCheck(metricsPort string) bool {
	return true
}

func getLocalIP() string {
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		return ""
	}
	for _, address := range addrs {
		// check the address type and if it is not a loopback the display it
		if ipnet, ok := address.(*net.IPNet); ok && !ipnet.IP.IsLoopback() {
			if ipnet.IP.To4() != nil {
				return ipnet.IP.String()
			}
		}
	}
	return ""
}

func getNodes(c *zk.Conn) []string {
	children, stat, err := c.Children("/")
	if err != nil {
		panic(err)
	}
	fmt.Println("Listing nodes")
	nodes := fmt.Sprintf("%+v %+v\n", children, stat)
	fmt.Println(nodes)
	return children
}

var sidecarsList = []string{
	"/beacon",
	"/mcrouter",
	"/metrics-agent",
	"/tcollector",
	// "/singer", Singer is deployed after the service so we do not check for it.
	"/zk_update_monitor",
}

func verifySidecarContainers() {
	cli, err := client.NewEnvClient()
	if err != nil {
		log.Fatalf("Unable to create docker client: %v", err)
	}

	ctx := context.Background()
	cli.NegotiateAPIVersion(ctx)
	containerList, err := cli.ContainerList(ctx, types.ContainerListOptions{All: false})
	if err != nil {
		log.Fatalf("Unable to retrieve list of containers: %v", err)
	}

	containerMap := make(map[string]bool)
	for _, container := range containerList {
		log.Printf("Adding '%s' to list of running containers.", container.Names[0])
		containerMap[container.Names[0]] = true
	}

	for _, sidecar := range sidecarsList {
		if _, ok := containerMap[sidecar]; !ok {
			log.Fatalf("%s sidecar is not running", sidecar)
		}
	}
}

func verifyFactsAndUUID() {
	var k = koanf.New(".")

	hostname, err := os.Hostname()
	if err != nil {
		log.Fatalf("Error retrieving hostname: %v", err)
	}

	pinfoFactsFile := fmt.Sprintf("/etc/facter/facts.d/pinfo-%s.yaml", hostname)
	if err := k.Load(file.Provider(pinfoFactsFile), yaml.Parser()); err != nil {
		log.Fatalf("error loading pinfo facts: %v", err)
	}

	if err := k.Load(file.Provider("/etc/facter/facts.d/nimbus.yaml"), yaml.Parser()); err != nil {
		log.Fatalf("error loading nimbus facts: %v", err)
	}

	instanceID, region := retrieveInstanceIDAndRegion()
	nimbusUUID := retrieveNimbusUUIDInstanceTag(region, instanceID)

	if k.String("nimbus_uuid") != *nimbusUUID {
		log.Fatalf("Facter nimbus UUID does not match EC2 tags nimbus UUID.")
	}

}

func retrieveInstanceIDAndRegion() (string, string) {
	sess := session.New()
	svcEC2metadata := ec2metadata.New(sess)
	instanceIdentity, err := svcEC2metadata.GetInstanceIdentityDocument()
	if err != nil {
		log.Fatalf("Failed to retrieve intance metadata: %v", err)
	}

	return instanceIdentity.InstanceID, instanceIdentity.Region
}

func retrieveNimbusUUIDInstanceTag(region string, instanceID string) *string {
	sess, err := session.NewSession(&aws.Config{
		Region: aws.String(region)},
	)
	if err != nil {
		log.Fatalf("Failed to open AWS session: %v", err)
	}

	svc := ec2.New(sess)
	input := &ec2.DescribeTagsInput{
		Filters: []*ec2.Filter{
			{
				Name: aws.String("resource-id"),
				Values: []*string{
					aws.String(instanceID),
				},
			},
		},
	}

	result, err := svc.DescribeTags(input)
	if err != nil {
		log.Fatalf("Failed to retrieve AWS Tags: %v", err)
	}

	for _, tag := range result.Tags {
		if *tag.Key == "pinterest.com/nimbus-uuid" {
			return tag.Value
		}
	}

	return nil
}
