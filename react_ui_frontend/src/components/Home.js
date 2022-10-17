import React, {useEffect, useState} from "react";

import UserService from "../services/user.service";
import Test from "./Test"

const Home = () => {
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

    return (
        <div className="container">
            <header className="jumbotron">
                Fufuf:
                <Test></Test>
                <h3>{content}</h3>
            </header>
        </div>
    );
};

export default Home;
