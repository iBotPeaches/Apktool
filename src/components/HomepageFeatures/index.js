import React from 'react';
import clsx from 'clsx';
import styles from './styles.module.css';

const FeatureList = [
  {
    title: 'Disassemble',
    description: (
      <>Apktool can help disassemble resources to nearly original form.</>
    ),
  },
  {
    title: 'Assemble',
    description: (
      <>
        Want to translate an app? Change a permission? Apktool can help you do
        that.
      </>
    ),
  },
  {
    title: 'Analyze',
    description: (
      <>
        Not interested in rebuilding? Just attach <code>-m</code> to any
        disassemble command. Apktool will do its best to rip apart the resources
        and manifest for easy inspection.
      </>
    ),
  },
];

function Feature({Svg, title, description}) {
  return (
    <div className={clsx('col col--4')}>
      <div className="text--center padding-horiz--md">
        <h3>{title}</h3>
        <p>{description}</p>
      </div>
    </div>
  );
}

export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          {FeatureList.map((props, idx) => (
            <Feature key={idx} {...props} />
          ))}
        </div>
      </div>
    </section>
  );
}
