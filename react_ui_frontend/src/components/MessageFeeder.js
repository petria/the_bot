import React, {useEffect} from "react";
import MessageFeedService from "../services/message-feed";

const MessageFeeder = () => {


    const fetchNewMessages = () => {
        let timestamp = Date.now() - (1000 * 5);
        if (timestamp !== null) {
            MessageFeedService.getMessagesSince(
                (response) => {
                    console.log(">> response", response.data);

                },
                (error) => {
                    console.log(">> error", error);
                },
                timestamp
            );
        }
    }

    useEffect(() => {
        const interval = setInterval(
            () => {
                fetchNewMessages();
            },
            5000);

        return () => clearInterval(interval);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    return (
        <div className="container">
            messageFeed
        </div>
    );
};

export default MessageFeeder;
