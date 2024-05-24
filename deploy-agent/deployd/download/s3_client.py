USE_BOTO3 = False
try:
    from boto.s3.connection import S3Connection
except ImportError:
    import botocore
    import boto3

    USE_BOTO3 = True


class Boto2Client:
    """
    Client to handle boto2 operations. This can be removed
    once boto2 is no longer used
    """
    def __init__(self, aws_access_key_id, aws_secret_access_key):
        self.client = S3Connection(aws_access_key_id, aws_secret_access_key, True)

    def get_key(self, bucket_name, key_name):
        """
        Return the object at the specified key
        """
        return self.client.get_bucket(bucket_name).get_key(key_name)

    def download_object_to_file(self, obj, file_name):
        """
        Download the object to the specified file name

        :param obj: the object returned from `self.get_key`
        :param file_name str: the file_name to download to
        """
        obj.get_contents_to_filename(file_name)

    def get_etag(self, obj):
        """
        Get the etag of the specified object

        :param obj: the object returned from `self.get_key`
        """
        return obj.etag


class Boto3Client:
    """
    Client to handle boto3 operations. This can be renamed to
    `S3Client` once boto2 is no longer used.
    """
    def __init__(self, aws_access_key_id, aws_secret_access_key):
        session = boto3.Session(
            aws_access_key_id=aws_access_key_id,
            aws_secret_access_key=aws_secret_access_key,
        )
        self.client = session.resource('s3')

    def get_key(self, bucket_name, key_name):
        """
        Return the object at the specified key
        """
        obj = self.client.Bucket(bucket_name).Object(key_name)
        try:
            # To be compatible with boto2, return None if key does not exist
            obj.load()
            return obj
        except botocore.exceptions.ClientError as e:
            if e.response['Error']['Code'] == "404":
                return None
            else:
                raise

    def download_object_to_file(self, obj, file_name):
        """
        Download the object to the specified file name

        :param obj: the object returned from `self.get_key`
        :param file_name str: the file_name to download to
        """
        obj.download_file(file_name)

    def get_etag(self, obj):
        """
        Get the etag of the specified object

        :param obj: the object returned from `self.get_key`
        """
        return obj.e_tag

S3Client = Boto3Client if USE_BOTO3 else Boto2Client
