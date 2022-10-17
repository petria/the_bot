import React, {useEffect, useState} from "react";
import MessageFeedService from "../services/message-feed-service";

const Test = () => {
    const [count, setCount] = useState(0);
    const [time, setTime] = useState(0);
    const [text, setText] = useState("fffu");
    const [doUpdate, setDoUpdate] = useState(1);

    useEffect(
        () => {

            const updateFeed = () => {
                MessageFeedService.getMessagesSince(
                    (response) => {
                        if (response.data.length > 0) {


                            console.log('data -> ', response.data);
                            const date = new Date(response.data[0].timestamp);
                            const msg = date + " " + response.data[0].sender.concat(" :: ").concat(response.data[0].message);

                            console.log('msg -> ', msg);


                            setText((text) => msg.concat("\n").concat(text));
                        }
                    },
                    (error) => {
                        // error
                    },
                    time
                );
            }


            if (count === 0) {
                setTime(666);
            } else {
                setTime(Date.now());
                if (doUpdate === 1) {
                    updateFeed();
                }
            }
        },
        [count]
    );


    useEffect(() => {
        let id = setTimeout(() => {
            setCount((count) => count + 1);
        }, 2000);
        return () => clearInterval(id);
    });

    const toggleUpdate = (e) => {
        if (doUpdate === 0) {
            setDoUpdate(1);
        } else {
            setDoUpdate(0);
        }
    }

    return (
        <>
            <div>
                <textarea readOnly="true" id="messages" cols="100" rows="10" value={text}></textarea>
                <button id="doUpdateToggle" onClick={ (e) => toggleUpdate(e) }>Toggle update</button>
                <button id="clear" onClick={ (e) => setText("") }>Clear</button>
            </div>
            <br/>

            count: {count}<br/>
            time: {time}<br/>
            doUpdate: {doUpdate}<br/>
        </>
    );

}


export default Test;


