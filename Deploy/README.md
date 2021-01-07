# Deploy DEPTest into RepairThemAll

1) First configure the [RepairThemAll](https://github.com/program-repair/RepairThemAll) (please refer to: [RepairThemAll usage](https://github.com/program-repair/RepairThemAll/blob/master/INSTALL.md)). 
Imagine that you have configured RepairThemAll correctly, and your RepairThemAll path is: `/apr/repairthemall`. Now do the following steps:

2) Copy file `purify.py` in this folder to `/apr/repairthemall/script/core/repair_tools/Purify.py`.


3) Modify file `/apr/repairthemall/script/core/utils.py`, and add 
```
import core.repair_tools.Purify
```

4) Create file: `/apr/repairthemall/data/repair_tools/purify.json`
The content of this file is:
```
{
	"name": "Purify",
	"version": "0.0.1",
	"jar": "purify.jar",
	"main": "apr.purify.Main"
}
```

If you have any questions please don't hesitate to contact me.
