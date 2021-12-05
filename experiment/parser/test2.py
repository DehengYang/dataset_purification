def _compare_keys(x, y):
    try:
        x = int(x)
    except ValueError:
        xint = False
    else:
        xint = True
    try:
        y = int(y)
    except ValueError:
        if xint:
            return -1
        return cmp(x.lower(), y.lower())
        # or cmp(x, y) if you want case sensitivity.
    else:
        if xint:
            return cmp(x, y)
        return 1

for k in sorted(a, cmp=_compare_keys):
    print(k, a[k]) # or whatever.