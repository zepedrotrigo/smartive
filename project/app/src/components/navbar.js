import React from 'react';

export class Navbar extends React.Component {

    render() {
        return (
            <nav className="navbar navbar-expand-lg navbar-light rounded my-4">
                <img src={process.env.PUBLIC_URL + '/logo192.png'} style={{maxHeight: "3rem", maxWidth: "3rem"}} alt="Logo"/>
                <button className="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarsExample09"
                        aria-controls="navbarsExample09" aria-expanded="false" aria-label="Toggle navigation">
                    <span className="navbar-toggler-icon"/>
                </button>

                <div className="collapse navbar-collapse" id="navbarsExample09">
                    <ul className="navbar-nav mx-3 fs-8 my-auto">
                        <li className="nav-item active mx-3">
                            <a className="nav-link fw-bolder text-black" href="/dashboard">DASHBOARD</a>
                        </li>
                        <li className="nav-item mx-3">
                            <a className="nav-link fw-bolder text-black" href="/devices">YOUR DEVICES</a>
                        </li>
                        <li className="nav-item mx-3">
                            <a className="nav-link fw-bolder text-black" href="/history">HISTORY</a>
                        </li>
                        <li className="nav-item mx-3">
                            <a className="nav-link fw-bolder text-black" href="/users">USERS</a>
                        </li>
                    </ul>
                </div>
            </nav>
        );

    }

}