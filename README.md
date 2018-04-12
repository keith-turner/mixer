# Mixer

Prototype for creating a derived graph by merging nodes from different social networks using
[Deriggy] and [Fluo].  Building this on Fluo has two benefits.  First, Fluo enables handling of very large graphs that do not fit on a single machine.  Second, Fluo makes it possible to continuously recompute the derived graph as the source data changes. This prototype goes with the [talk](https://youtu.be/oqrjEexMLVE) given at the [Accumulo
Summit](http://accumulosummit.com/) in 2017. 

## Running Mixer

Mixer is best explained by examples. To get started, Mixer has a shell that supports the following commands :

```
$ ./mixer.sh shell fluo.properties 
>help
Commands : 

	follow <graph> <user id 1> <user id 2>
	unfollow <graph> <user id 1> <user id 2>
	alias <graph> <user id> <alias>
	unalias <graph> <user id>
	lookup <id>
	load <graph> <file>
	setattrs <graph> <id> {<key>=<value>}
	print
	help
	exit|quit
```

Inorder to run Mixer, a Fluo instance needs to be running.  The following command will start a single machine Fluo development instance.  After Fluo starts, a `fluo.properties` file containing connection information is created.  Wait for this file to exists before proceeding.

```bash
./mixer.sh mini &> mini.log &
```

The following adds edges to multiple social network graphs.  For example assume `tw` represents Twitter, then in the Twitter graph `bob1998` follows `bigjoe`.

```bash
$ ./mixer.sh shell fluo.properties <<EOF
follow tw bob1998 bigjoe
follow fb bjoe rob
follow tw bob1998 alice
follow gh joe42 alice
follow tw bob1998 susan71
follow fb suzy71 bob98
follow g+ susandoe robby
follow fb suzy71 bob98
exit
EOF
```

The following visualizes the derived graph in the external query table.


```bash
sudo apt install graphviz
```

```bash
./mixer.sh graphviz fluo.properties | neato -Tpng > sgraph1.png; xdg-open sgraph1.png
```

![graph 1](images/sgraph1.png)

The following maps users in different social graphs into the derived graph.  For example the Twitter user `bob1998` and the Facebook user `bob98` are  mapped to `bob` in the derived graph. 

```bash
$ ./mixer.sh shell fluo.properties <<EOF
alias tw bob1998 bob
alias tw bigjoe joe
alias fb bjoe joe
alias fb rob rob
alias tw alice alice
alias gh joe42 joe
alias gh alice alice
alias tw susan71 susan
alias fb suzy71 susan
alias fb bob98 bob
alias g+ susandoe susan
alias g+ robby rob
exit
EOF
```

After mapping the users, the derived graph looks much different.

```bash
./mixer.sh graphviz fluo.properties | neato -Tpng > sgraph2.png; xdg-open sgraph2.png
```

![graph 2](images/sgraph2.png)

The visualization does not currently show attributes. The following shows an example of how
attributes will automatically be mapped into the derived graph.

```bash
$ ./mixer.sh shell fluo.properties
>lookup joe
  joe <- bob        followers=1,following=3,rawEdges=1
  joe -> alice      followers=2,following=0,rawEdges=1
  joe -> rob        followers=2,following=0,rawEdges=1
>setattrs tw bob1998 loc=TX
>setattrs fb bob98 bday=8/13
>lookup joe
  joe <- bob        followers=1,following=3,loc=TX,bday=8/13,rawEdges=1
  joe -> alice      followers=2,following=0,rawEdges=1
  joe -> rob        followers=2,following=0,rawEdges=1
>lookup alice
  alice <- bob        followers=1,following=3,loc=TX,bday=8/13,rawEdges=1
  alice <- joe        followers=1,following=2,rawEdges=1
```

There are two interesting things happening here.  First, the attributes are mapped from the Twitter and Facebook users for bob into the derived graph.  Second, when this happens all of bob's neighbors in the derived graph are updated with the attribute information.  The same is
true for the folllowing counts, when these change for a node all of its neighbors are updated 
in the query table.  The derived graph is computed in Fluo and exported to an Accumulo table for query.  Every time an edge, alias, or attribute changes the derived graph is updated.

Removing `fluo.properties` causes MiniFluo to stop.

```bash
rm fluo.properties
```

[Deriggy]: https://github.com/keith-turner/deriggy
[Fluo]: https://fluo.apache.org
