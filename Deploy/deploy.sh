purifyJar="/home/apr/apr_tools/datset_purification_2020/purification/purify/target/purify-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
targetPath="/mnt/recursive-repairthemall/RepairThemAll-Nopol-github/repair_tools/purify.jar "


BASE="$(cd $(dirname $0); pwd)"
cd $BASE

cp $purifyJar $targetPath

time=$(date "+%Y-%m-%d %H:%M:%S")

echo -e "$time --- cp $purifyJar $targetPath\n"
echo -e "$time --- cp $purifyJar $targetPath\n" >> $BASE/deploy.log
