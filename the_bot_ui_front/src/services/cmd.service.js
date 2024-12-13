import axios from 'axios';
import authHeader from './auth-header';

const API_URL = 'http://localhost:8200/api/test/cmd/';

class CmdService {

    getData(command) {
        return axios.get(API_URL + command, {headers: authHeader()});
    }
}

export default new CmdService();