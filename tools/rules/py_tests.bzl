def py_tests(name, srcs, mains=[], **kwargs):
    for main in _get_mains(srcs, mains):
        test_name = name + "_" + main.replace("-", "_")
        test_name = test_name.replace("/", "_")
        test_name = test_name.replace(".", "_")
        native.py_test(
            name = test_name,
            srcs = srcs,
            main = main,
            **kwargs
        )

def _get_mains(srcs, mains):
    if mains:
        return mains
    names = []
    # scan srcs and generate mains
    for src in srcs:
        file = src.split("/")[-1]
        if not file.startswith("test"):
            continue
        names.append(src)
    return names
