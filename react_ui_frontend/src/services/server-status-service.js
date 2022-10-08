import axios from "axios";
//import authHeader from "./auth-header";

const API_URL = "http://localhost:8080/api/server_config/";

const getServerStatus = (ok, error) => {

    axios.get(API_URL)
        .then(
            (response) => {
//                console.log(">>> response ", response);
                ok(response);
            }
        )
        .catch(
            (response) => {
//                console.log(">>> error ", response);
                error(response);
            }
        );

}
/*

    return axios.get(API_URL)
        .then(
            (response) => {
                console.log(">> response", response);
            }
        )
        .catch(function (error) {
            if (error.response) {
                // The request was made and the server responded with a status code
                // that falls out of the range of 2xx
                console.log("D: " + error.response.data);
                console.log("S: " + error.response.status);
                console.log("H: " +error.response.headers);
            } else if (error.request) {
                // The request was made but no response was received
                // `error.request` is an instance of XMLHttpRequest in the browser and an instance of
                // http.ClientRequest in node.js
                console.log("2: " + error.request);
            } else {
                // Something happened in setting up the request that triggered an Error
                console.log('3: Error', error.message);
            }
            console.log("4:", error.config);
        });}
*/

const ServerStatusService = {
    getServerStatus
};

export default ServerStatusService;