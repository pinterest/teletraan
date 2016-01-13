from deployd import IS_PINTEREST


class DefaultStatsdTimer(object):
    def __enter__(self):
        pass

    def __exit__(self, stats, sample_rate, tags):
        pass


def create_stats_timer(stats, sample_rate, tags):
    if IS_PINTEREST:
        from pinstatsd.statsd import statsd_context_timer
        return statsd_context_timer(entry_name=stats, sample_rate=sample_rate, tags=tags)
    else:
        return DefaultStatsdTimer()


def create_sc_increment(stats, sample_rate, tags):
    if IS_PINTEREST:
        from pinstatsd.statsd import sc
        sc.increment(stats, sample_rate, tags)
    else:
        return
