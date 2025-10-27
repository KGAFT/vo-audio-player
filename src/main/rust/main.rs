use byteorder::{ReadBytesExt};
use std::{
    io::{Read, Seek},
};
use torznab::Client;

pub mod j_objects;
pub mod jni_interface;
pub mod operative;
pub mod util;
#[tokio::main]
async fn main() {
    let client = Client::new("http://127.0.0.1:9117/api/v2.0/indexers/rutracker/results/torznab/", "dqzu3f7kzpsgjrxg5916n2p7lc6o9mwl").unwrap();
    client.audiosearch(Some("korn")).await.unwrap().iter().for_each(|item|{
        if item.is_ok(){
            let item = item.as_ref().unwrap();
            println!("{}", item.name);
        }

    })
}
