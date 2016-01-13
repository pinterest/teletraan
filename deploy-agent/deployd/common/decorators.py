import time


def singleton(non_singleton_cls):
    """Decorator to make sure there is only one instance of a class.
    Args:
        cls: A class.

    Returns:
        A class.

    """
    instances = {}

    def getinstance(*args, **kwargs):
        if non_singleton_cls not in instances:
            instances[non_singleton_cls] = non_singleton_cls(*args, **kwargs)
        return instances[non_singleton_cls]

    # Save the class for mocking purposes.
    getinstance._cls = non_singleton_cls
    return getinstance


def retry(ExceptionToCheck, tries=4, delay=3, backoff=2):
    """Retry calling the decorated function using an exponential backoff.

    Similar to utils.decorators.retry() but customized slightly for operations tools.

    :param ExceptionToCheck: the exception to check. may be a tuple of
        excpetions to check
    :type ExceptionToCheck: Exception or tuple
    :param tries: number of times to try (not retry) before giving up
    :type tries: int
    :param delay: initial delay between retries in seconds
    :type delay: int
    :param backoff: backoff multiplier e.g. value of 2 will double the delay
        each retry
    :type backoff: int"""

    def deco_retry(f):
        def f_retry(*args, **kwargs):
            mtries, mdelay = tries, delay
            while mtries > 1:
                try:
                    return f(*args, **kwargs)
                except ExceptionToCheck:
                    time.sleep(mdelay)
                    mtries -= 1
                    mdelay *= backoff
            return f(*args, **kwargs)

        return f_retry  # true decorator

    return deco_retry
