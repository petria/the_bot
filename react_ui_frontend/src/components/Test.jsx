import React, {useEffect, useState} from "react";
import MessageFeedService from "../services/message-feed-service";

const Test = () => {
    const [count, setCount] = useState(0);
    const [time, setTime] = useState(0);
    const [text, setText] = useState("");
    const [doUpdate, setDoUpdate] = useState(1);
    const [afterId, setAfterId] = useState(0);

    useEffect(() => {

        const firstFetch = () => {
            MessageFeedService.getLastMessages((response) => {
                if (response.data.length > 0) {
                    let msgs = "";
                    let lastId = 0;
                    for (let i = 0; i < response.data.length; i++) {
                        const m = response.data[i];
                        const msg = m.id + " :: " + m.sender.concat(" :: ").concat(m.message);
                        msgs = msgs.concat(msg).concat('\n');
                        lastId = m.id;
                    }
                    console.log('>>>msgs ', msgs);
                    setAfterId(lastId);
                    setText((text) => text.concat(msgs));
                }
            }, (error) => {
                // error
            }, 10);
        };

        const updateFetch = () => {
            MessageFeedService.getMessagesAfterId((response) => {
                if (response.data.length > 0) {
                    let msgs = "";
                    let lastId = 0;
                    for (let i = 0; i < response.data.length; i++) {
                        const m = response.data[i];
                        const msg = m.id + " :: " + m.sender.concat(" :: ").concat(m.message);
                        msgs = msgs.concat(msg).concat('\n');
                        lastId = m.id;
                    }
                    console.log('>>msgs ', msgs);
                    setAfterId(lastId);
                    setText((text) => text.concat(msgs));
                }
            }, (error) => {
                // error
            }, afterId);
        }

        if (count === 0) {
            firstFetch();
        } else {
            setTime(Date.now());
            if (doUpdate === 1) {
                updateFetch();
            }
        }
    }, [count]);


    useEffect(() => {
        let id = setTimeout(() => {
            setCount((count) => count + 1);
        }, 5000);
        return () => clearInterval(id);
    });

    const toggleUpdate = (e) => {
        if (doUpdate === 0) {
            setDoUpdate(1);
        } else {
            setDoUpdate(0);
        }
    }

    return (<>
        <div>
            <textarea readOnly="true" id="messages" cols="100" rows="10" value={text}></textarea>
            <button id="doUpdateToggle" onClick={(e) => toggleUpdate(e)}>Toggle update</button>
            <button id="clear" onClick={(e) => setText("")}>Clear</button>
        </div>
        <br/>

        count: {count}<br/>
        time: {time}<br/>
        doUpdate: {doUpdate}<br/>
        afterId: {afterId}<br/>
    </>);

}


export default Test;


