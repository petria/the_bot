import {useState} from "react";
import cmdService from "../services/cmd.service";

export default function CommandSend() {

    const [value, changeValue] = useState('Results:\n\n\n');
    const [inputValue, setInputValue] = useState('');

    const handleInputChange = (event) => {
        setInputValue(event.target.value);
    };

    const handleChange = (text) => {
        console.log(text)
        // text = JSON.stringify(text)?JSON.stringify(text):text;
        changeValue(v => v + "\n" + text.data)
    }

    const tArea = <textarea rows="20" cols="50" readOnly value={value}></textarea>
    const tInput = <input type="text" value={inputValue} onChange={handleInputChange}></input>

    const send = async () => {
        handleChange(await runCommand(inputValue));
    }

    const sendButton = <button onClick={send}>Send</button>

    const runCommand =
        async (textVal) => {
            try {
                let v = await cmdService.getData(textVal);
                return v;
            } catch (error) {
                return error + "";
            }
        }

    const st = {
        margin: "0 auto",
        padding: "0 50%"
    }

    return (
        <div style={st}>
            {tArea}
            <br></br>
            {tInput}
            {sendButton}
        </div>);

};