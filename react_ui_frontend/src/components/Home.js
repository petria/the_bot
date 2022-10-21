import React, {useEffect, useState} from "react";

import UserService from "../services/user.service";
import ReactTabs from './ReactTabs'
import ChannelFeed from './messagefeed/ChannelFeed';


const Home = (props) => {
    const [content, setContent] = useState("");

    useEffect(() => {
        UserService.getPublicContent().then(
            (response) => {
                setContent(response.data);
            },
            (error) => {
                const _content =
                    (error.response && error.response.data) ||
                    error.message ||
                    error.toString();

                setContent(_content);
            }
        );
    }, []);
//<!--                <Test></Test>-->
    console.log('>>> props', props);

    return (
        <div className="container">
            <h3>
                BotName: {props.runtime.config.botConfig.botName}
            </h3>
            <header className="jumbotron">
                <br/>
                <ChannelFeed title='#HokanDEV'></ChannelFeed>
                <ReactTabs></ReactTabs>
                <h3>{content}</h3>
            </header>
        </div>
    );
};

export default Home;
