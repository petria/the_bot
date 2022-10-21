import React, {useEffect, useState} from "react";
import {Link, Route, Routes, useNavigate} from "react-router-dom";
import "bootstrap/dist/css/bootstrap.min.css";
import 'react-notifications/lib/notifications.css';
import "./App.css";

import AuthService from "./services/auth.service";
import TheBotConfigService from "./services/the-bot-config-service";

import Login from "./components/Login";
import Register from "./components/Register";
import Home from "./components/Home";
import Profile from "./components/Profile";
import BoardUser from "./components/BoardUser";
import BoardModerator from "./components/BoardModerator";
import BoardAdmin from "./components/BoardAdmin";
import BoardServerStatus from "./components/BoardServerStatus"
import EventBus from "./common/EventBus";
import {NotificationManager} from "react-notifications";

const App = () => {
    const [showModeratorBoard, setShowModeratorBoard] = useState(false);
    const [showAdminBoard, setShowAdminBoard] = useState(false);
    const [currentUser, setCurrentUser] = useState(undefined);

    const [runtime, setRuntime]
        = useState(
        {
            config: null
        }
    );

    let navigate = useNavigate();
    const logOut = () => {
        console.log("App.js doing logOut() !!!");

        AuthService.logout();
        setShowModeratorBoard(false);
        setShowAdminBoard(false);
        setCurrentUser(undefined);

        NotificationManager.error("You have been logged off!", null, 5000, null);

        navigate("/home");

    };
    useEffect(() => {
//        console.log('Importing The Bot Config from backend');
        TheBotConfigService.getBotConfig(
            (response) => {
                console.log('TheBotConfig -> ', response.data);
                setRuntime(
                    values => ({
                        ...values,
                        config: response.data
                    })
                );
            },
            (error) => {
                // todo
            }
        );
    }, []);


    useEffect(() => {
        const user = AuthService.getCurrentUser();

        if (user) {
            setCurrentUser(user);
            setShowModeratorBoard(user.roles.includes("ROLE_MODERATOR"));
            setShowAdminBoard(user.roles.includes("ROLE_ADMIN"));
        }

        EventBus.on("logout", () => {
            logOut();
        });

        return () => {
            EventBus.remove("logout");
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);


    return (
        <div>
            <nav className="navbar navbar-expand navbar-dark bg-dark">
                <Link to={"/"} className="navbar-brand">
                    bezKoder
                </Link>
                <div className="navbar-nav mr-auto">
                    <li className="nav-item">
                        <Link to={"/home"} className="nav-link">
                            Home
                        </Link>
                    </li>

                    <li className="nav-item">
                        <Link to={"/server_status"} className="nav-link">
                            Server status
                        </Link>
                    </li>

                    {showModeratorBoard && (
                        <li className="nav-item">
                            <Link to={"/mod"} className="nav-link">
                                Moderator Board
                            </Link>
                        </li>
                    )}

                    {showAdminBoard && (
                        <li className="nav-item">
                            <Link to={"/admin"} className="nav-link">
                                Admin Board
                            </Link>
                        </li>
                    )}

                    {currentUser && (
                        <li className="nav-item">
                            <Link to={"/user"} className="nav-link">
                                User
                            </Link>
                        </li>
                    )}
                </div>

                {currentUser ? (
                    <div className="navbar-nav ml-auto">
                        <li className="nav-item">
                            <Link to={"/profile"} className="nav-link">
                                {currentUser.username}
                            </Link>
                        </li>
                        <li className="nav-item">
                            <a href="/login" className="nav-link" onClick={logOut}>
                                LogOut
                            </a>
                        </li>
                    </div>
                ) : (
                    <div className="navbar-nav ml-auto">
                        <li className="nav-item">
                            <Link to={"/login"} className="nav-link">
                                Login
                            </Link>
                        </li>

                        <li className="nav-item">
                            <Link to={"/register"} className="nav-link">
                                Sign Up
                            </Link>
                        </li>
                    </div>
                )}
            </nav>

            {
                runtime.config === null
                    ?
                    <div>
                        loading..
                    </div>
                    :
                    <div>
                        <div className="container mt-3">

                            <Routes>
                                <Route path="/" element={<Home runtime={runtime}/>}/>
                                <Route path="/server_status" element={<BoardServerStatus/>}/>
                                <Route path="/home" element={<Home runtime={runtime}/>}/>
                                <Route path="/login" element={<Login/>}/>
                                <Route path="/register" element={<Register/>}/>
                                <Route path="/profile" element={<Profile/>}/>
                                <Route path="/user" element={<BoardUser/>}/>
                                <Route path="/mod" element={<BoardModerator/>}/>
                                <Route path="/admin" element={<BoardAdmin/>}/>
                            </Routes>

                        </div>
                    </div>

            }

        </div>
    );
};

export default App;
