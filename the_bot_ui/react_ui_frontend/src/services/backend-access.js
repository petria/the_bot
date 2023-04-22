import axios from "axios";
import EventBus from "../common/EventBus";

const check_not_authorized = (status) => {
//    console.log("Check not authorized status: " + status);
    if (status === 401) {
        EventBus.dispatch("logout");
    }

}

const http_get = (url, ok, error, notify = false) => {

    axios.get(url)
        .then(
            (response) => {
                if (notify === true) {
                    check_not_authorized(response.status);
                }
                ok(response);
            }
        )
        .catch(
            (response) => {
                if (notify === true) {
                    check_not_authorized(response.status);
                }
                error(response);
            }
        );

}

const BackendAccess = {
    http_get
};

export default BackendAccess;

