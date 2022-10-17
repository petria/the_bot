import React, {useEffect, useState} from "react";

const Test = () => {
    const [count, setCount] = useState(0);
    const [time, setTime] = useState(0);
    const [text, setText] = useState("fffu");
    const [doUpdate, setDoUpdate] = useState(1);

    useEffect(
        () => {
            if (count === 0) {
                setTime(666);
            } else {
                setTime(Date.now());
                if (doUpdate === 1) {
                    setText((text) => "221".concat("\n").concat(text));
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


