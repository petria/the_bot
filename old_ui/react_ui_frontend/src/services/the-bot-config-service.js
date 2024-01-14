import BackendAccess from "./backend-access";

const API_URL = "http://localhost:8080/api/the_bot_config/";

const getBotConfig = (ok, error) => {
    const url = API_URL;
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


const TheBotConfigService = {
    getBotConfig,
};


export default TheBotConfigService;
