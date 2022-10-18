import BackendAccess from "./backend-access";

const API_URL = "http://localhost:8080/api/message_feed/";

const getMessagesAfterId = (ok, error, id) => {
    const url = API_URL.concat("after_id/").concat(id);
//    console.log(">> url: ", url);
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


const getLastMessages = (ok, error, max) => {
    const url = API_URL.concat("last/").concat(max);
//    console.log(">> url: ", url);
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


const getMessagesSince = (ok, error, timestamp) => {
    const url = API_URL.concat("since/").concat(timestamp);
//    console.log(">> url: ", url);

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
    getLastMessages,
    getMessagesAfterId,
    getMessagesSince
}

export default MessageFeedService;