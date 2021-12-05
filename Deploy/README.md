# integrate to RepairThemAll

1) add file:
`/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/core/repair_tools/Purify.py`
(this is from `/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/core/repair_tools/APR.py`)

replace all APR/apr with Purify/purify

2) modify file `/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/core/utils.py`:

add `import core.repair_tools.Purify`

3) add file: `/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/data/repair_tools/purify.json`
(this is from `/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/data/repair_tools/apr.json`) 

the content is:

```
{
	"name": "Purify",
	"version": "0.0.1",
	"jar": "purify.jar",
	"main": "apr.purify.Main"
}
```

4) add file `/home/apr/apr_tools/datset_purification_2020/purification/purify/README/deploy.sh`. 

```
purifyJar="/home/apr/apr_tools/datset_purification_2020/purification/purify/target/purify-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
targetPath="/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/repair_tools/purify.jar "


BASE="$(cd $(dirname $0); pwd)"
cd $BASE

cp $purifyJar $targetPath

time=$(date "+%Y-%m-%d %H:%M:%S")

echo -e "$time --- cp $purifyJar $targetPath\n"
echo -e "$time --- cp $purifyJar $targetPath\n" >> $BASE/deploy.log
```


5) further modify `/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/script/core/repair_tools/Purify.py` for passing parameters to `purify.jar`.


