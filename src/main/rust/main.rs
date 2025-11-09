use byteorder::{ReadBytesExt};
use std::{
    io::{Read, Seek},
};
use std::fs::File;
use torznab::Client;
use crate::j_objects::track::Track;

pub mod j_objects;
pub mod jni_interface;
pub mod operative;
pub mod util;
#[tokio::main]
async fn main() {
    /*
    let client = Client::new("http://127.0.0.1:9117/api/v2.0/indexers/rutracker/results/torznab/", "dqzu3f7kzpsgjrxg5916n2p7lc6o9mwl").unwrap();
    client.moviesearch(Some("snatch")).await.unwrap().iter().for_each(|item|{
        if item.is_ok(){
            let item = item.as_ref().unwrap();
            println!("{}", item.name);
        }
    })

     */
    let meta = Track::extract_dff_metadata("F:\\Music\\SACD_Michael Jackson_Bad - 1987 (Epic 28.3P-800 LP-Japan)\\01. Bad.dff").unwrap();
    meta.tags.iter().for_each(|tag|{
        println!("{},{}",tag.0,tag.1);
    });
    if meta.id3_raw.is_some(){
       meta.id3_raw.as_ref().unwrap().iter().for_each(|i|{
           print!("{}", i);
       })
    }
}
