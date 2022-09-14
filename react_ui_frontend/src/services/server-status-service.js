import axios from "axios";
import authHeader from "./auth-header";

const API_URL = "http://localhost:8080/api/server_config/";

const getServerStatus = () => {

    return axios.get(API_URL);

/*    var data = {
        "servers": [
            {
                "id": "1",
                "name": "server1",
                "type": "IRC",
                "status": "online"
            },
            {
                "id": "2",
                "name": "server2",
                "type": "Telegram",
                "status": "offLine"
            }
        ]
    };
    return data;*/
}

const ServerStatusService = {
    getServerStatus
};

export default ServerStatusService;