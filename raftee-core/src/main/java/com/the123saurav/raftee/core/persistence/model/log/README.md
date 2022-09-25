## Log Record format
Illustrated in Entry.java

We only need to travel backwards in case of log mismatch between leader and follower.  
We can optimize on that by storing some data in log, but that would add to storage.
Another approach is that we keep a sparse index in memory:
    {"term": [{"logIndexK": offset1, "logIndexK+10000": offset2, ...}]} 
This array is sorted and we can do binary search based on leader's response and then read file
sequentially from there. This is a pattern used for example in SSTs where we do BS to find ceiling and
do IO on SST file to discover the key.
We can tune this index to make a tradeoff b/n memory and IO.

We may want to trim the old terms in the future.


