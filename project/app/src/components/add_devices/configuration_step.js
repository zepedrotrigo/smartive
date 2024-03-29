import React from 'react';

import Button from "react-bootstrap/Button";
import Container from "react-bootstrap/Container";

export class ConfigurationStep extends React.Component {

    render() {

        return (

            <div>

                <p className="fw-bold mb-4">{this.props.title}</p>

                <Container className="mt-4">
                    {this.props.childComponent}
                </Container>

                <Button id="configStepNextBtn" className="mt-4 w-25 disabled" style={{backgroundColor: "#f76540", borderColor: "#f76540"}} onClick={() => this.props.on_next_click()}>{this.props.btn_text}</Button>

            </div>

        );

    }

}