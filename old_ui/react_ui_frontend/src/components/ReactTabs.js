import React, {useState} from 'react'
import {Tab, Tabs} from 'react-bootstrap'

export default function ReactTabs() {

    const [tabKey, initTabKey] = useState('one')

    return (
        <div>
            <h2 className="mb-3">React Js Tabs Component Example</h2>
            <Tabs activeKey={tabKey} onSelect={(e) => initTabKey(e)}>
                <Tab eventKey="one" title="Tab 1">
                    <p>Tab 1</p>
                    <p>
                        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Ut sed enim
                        semper mi congue vestibulum.
                    </p>
                </Tab>
                <Tab eventKey="two" title="Tab 2">
                    <p>Tab 2</p>
                </Tab>
                <Tab eventKey="three" title="Tab 3">
                    <p>Tab 3</p>
                </Tab>
                <Tab eventKey="four" title="Tab 4">
                    <p>Tab 4</p>
                </Tab>
            </Tabs>
        </div>
    )
}
