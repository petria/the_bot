import React, {useEffect, useRef, useState} from "react";
import MessageFeedService from "../../services/message-feed-service";
import Moment from 'moment';

const ChannelFeed = (props) => {

    const [count, setCount] = useState(0);
    const [time, setTime] = useState(0);
    const [text, setText] = useState("");
    const [doUpdate, setDoUpdate] = useState(1);
    const [lastMsgId, setLastMsgId] = useState(0);

    const textArea = useRef();

    useEffect(() => {

        const updateFetch = () => {
            MessageFeedService.getMessagesAfterId((response) => {
                if (response.data.length > 0) {
                    let msgs = "";
                    let lastId = 0;
                    for (let i = 0; i < response.data.length; i++) {
                        const m = response.data[i];

                        const mm = Moment(m.timestamp).format('HH:mm:ss')
                        const source = m.messageSource;
                        const msg = source.concat(" -> ").concat(mm).concat(" ").concat(m.sender).concat(" :: ").concat(m.message);
                        msgs = msgs.concat(msg).concat('\n');
                        lastId = m.id;
                    }
                    console.log('>>msgs ', msgs);
                    setLastMsgId(lastId);
                    setText((text) => text.concat(msgs));
                    const area = textArea.current;
                    area.scrollTop = area.scrollHeight;
                }
            }, (error) => {
                // error
            }, lastMsgId);
        }

        if (doUpdate === 1) {
            updateFetch();
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
        <div className="container">
            <div>
                <h3>{props.title}</h3>
            </div>
            <div>
                <textarea className="message-feed-textarea" ref={textArea} readOnly="true" id="messages" cols="100"
                          rows="10" value={text}></textarea>
            </div>
            <div className="container">
                <button id="doUpdateToggle" onClick={(e) => toggleUpdate(e)}>Toggle update</button>
                <button id="clear" onClick={(e) => setText("")}>Clear</button>
            </div>
        </div>
        <br/>

        count: {count}<br/>
        time: {time}<br/>
        doUpdate: {doUpdate}<br/>
        lastMsgId: {lastMsgId}<br/>
    </>);

}


export default ChannelFeed;


