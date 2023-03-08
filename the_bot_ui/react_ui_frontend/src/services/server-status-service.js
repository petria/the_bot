import BackendAccess from "./backend-access";

const API_URL = "http://localhost:8080/api/server_config/";

const getServerStatus = (ok, error) => {

    BackendAccess.http_get(
        API_URL,
        (response) => {
            ok(response);
        },
        (error_response) => {
            error(error_response);
        },
        true
    );

}

const ServerStatusService = {
    getServerStatus
};

export default ServerStatusService;