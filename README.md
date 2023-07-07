# ChainChat
**Blockchain-powered chat application using JavaFX and TomP2P, providing decentralized, secure messaging with immutable message history. Requires JavaFX to be installed.**

### STORAGE:
Data such as blocks, headers and contact info are stored in LevelDB database, format (key:value) :

```
index db store:       f+filename : indexFile (eg. fblocks0 : file.header) 
                      b+blockhash : indexBlock (eg. b00f3c.. : block.header)
                
block db store:       blockhash : block
contacts db store:    username : pubKey
```

### CONNECTION:
TomP2P (https://tomp2p.net/) is used for P2P connection. Peers communicate by sending direct 
messages to each other – eg. sending newly mined blocks, requesting a missing block, asking for 
their chaintip, etc..
When app is launched, user needs to enter their own port and some other peer IP that is connected 
to the network (bootstrap peer is not necessary to use app, as this user can be the first one in the 
system). If boostrapping is correct, 
1. The connecting user asks all online peers for their chaintip (most recent block), then after waiting 
some time for users to reply, all received chaintips are evaluated.
2. Chaintip with highest total difficulty is picked, the peer with that chaintip is queried to ask for a 
chain of headers; but not a full chain of headers from “genesis”. User checks his own current chain 
and goes back 30 blocks (by 30 blocks consensus should be reached in the network with a very high 
probability), takes the hash of that block and instead requests chain of headers from that block. This 
is done to relieve networking load, as especially when the number of blocks gets very large, sending 
the whole chain of tens or hundreds of thousands of blocks will take a lot of time and most of that 
data will be wasted if connecting user is only missing a few blocks..
3. After receiving that chain of headers, it is then validated -> checking if it is mined, checking if 
difficulty levels are correct, etc. if all headers are valid, then full blocks are requested..
4. To save time, validation on these full blocks is not repeated -> hashes of blocks whose headers 
were already validated are stored in a linkedlist, and if received block hash is inside that linkedlist, it 
is simply stored. 
5. When the storing of all blocks is finished, the user has synced back up with the network and can 
start messaging..

### BLOCKCHAIN:
Block time is currently at 20sec (so for a user to send a message and get a response it should take 
around ~40sec). For block data to be confirmed with high probability, it needs to be buried by ~15 
blocks, so messages can be considered safe after about 5 minutes. Block difficulty is based on avg. 
number of hashes required to solve ‘proof-of-work’ challenge. If the time difference between blocks 
is under 20sec, difficulty is doubled, if it is over 20sec, difficulty is divided by 2..

### MESSAGING:
To send a message, the message is encrypted and all the metadata (receiver pubKey, sender pubKey, 
signature, AES key, date, etc.) is stored inside a “Message” object, which is then distributed to all 
connected peers. Upon receiving it, peers store this message inside a temporary file (which is 
created/deleted every time the app is opened/closed), and when a peer wants to mine a new block, 
all the messages are loaded from the file and a merkle root is generated.

When opening messages tab, the full chain is checked to find any messages directed to the user. If 
some message is found, the contacts DB is checked to see if user knows this other user (if they do, a 
conversation “box” is added with their username, else, the user is added to the users contacts DB 
named as “unknown1”, “unknown2”, etc). The messages have little bubbles to them to indicate their status –
more info on that can be found in the “blockchain” tab inside the app.
