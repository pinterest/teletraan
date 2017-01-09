_PREFIXES = ("org", "com", "edu")

def java_tests(name, srcs, test_classes=[], **kwargs):
    for test_class in _get_test_classes(srcs, test_classes):
        native.java_test(
            name = name + "_" + test_class.replace(".", "_"),
            srcs = srcs,
            test_class = test_class,
            **kwargs
        )

def _get_test_classes(srcs, test_classes):
    if test_classes:
        return test_classes
    names = []
    # scan srcs and generate test_classes
    for src in srcs:
        if not src.endswith("Test.java"):
            continue
        names.append(_get_class_name(src))
    return names

def _get_class_name(path):
    parts = []
    found = False
    for part in path[:-5].split("/"):
        if found:
            parts.append(part)
        else:
            if part in _PREFIXES:
                parts.append(part)
                found = True
    if not found:
        print("Unexpected test file " + path)
        return None
    return ".".join(parts)


