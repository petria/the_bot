import BackendAccess from "./backend-access";

const API_URL = "http://localhost:8080/api/message_feed/since/";


const getMessagesSince = (ok, error, timestamp) => {
    const url = API_URL.concat(timestamp);
    console.log(">> url: ", url);

    BackendAccess.http_get(
        url,
        (response) => {
            ok(response);
        },
        (error_response) => {
            error(error_response);
        },
        true
    );

}

const MessageFeedService = {
    getMessagesSince
}

export default MessageFeedService;