class Build(object):

    def __init__(self, jsonValue=None):
        self.buildId = None
        self.buildName = None
        self.buildVersion = None
        self.artifactUrl = None
        self.scm = None
        self.scmRepo = None
        self.scmBranch = None
        self.scmCommit = None
        self.scmInfo = None
        self.commitDate = None
        self.publishInfo = None
        self.publishDate = None

        if jsonValue:
            self.buildId = jsonValue.get('id')
            self.buildName = jsonValue.get('name')
            self.buildVersion = jsonValue.get('commitShort')
            self.artifactUrl = jsonValue.get('artifactUrl')
            self.scm = jsonValue.get('type')
            self.scmRepo = jsonValue.get('repo')
            self.scmBranch = jsonValue.get('branch')
            self.scmCommit = jsonValue.get('commit')
            self.scmInfo = jsonValue.get('commitInfo')
            self.commitDate = jsonValue.get('commitDate')
            self.publishInfo = jsonValue.get('publishInfo')
            self.publishDate = jsonValue.get('publishDate')

    def __str__(self):
        return "Build(buildId={}, buildName={}, buildVersion={}, artifactUrl={}, scm={}, " \
               "scmRepo={}, scmBranch={}, scmCommit={}, scmInfo={}, commitDate={}, publishInfo={}, " \
               "publishDate={})".format(self.buildId, self.buildName, self.buildVersion,
                                        self.artifactUrl, self.scm, self.scmRepo, self.scmBranch,
                                        self.scmCommit, self.scmInfo, self.commitDate,
                                        self.publishInfo, self.publishDate)
