/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 */

import React, {useEffect} from 'react';
import type {PropsWithChildren} from 'react';
import {
  Button,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Text,
  useColorScheme,
  View,
} from 'react-native';
import BTClient from '@fluzclient/react-native-braintree-no-ui';
import {
  Colors,
  DebugInstructions,
  Header,
  LearnMoreLinks,
  ReloadInstructions,
} from 'react-native/Libraries/NewAppScreen';

type SectionProps = PropsWithChildren<{
  title: string;
}>;

function Section({children, title}: SectionProps): React.JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';
  return (
    <View style={styles.sectionContainer}>
      <Text
        style={[
          styles.sectionTitle,
          {
            color: isDarkMode ? Colors.white : Colors.black,
          },
        ]}>
        {title}
      </Text>
      <Text
        style={[
          styles.sectionDescription,
          {
            color: isDarkMode ? Colors.light : Colors.dark,
          },
        ]}>
        {children}
      </Text>
    </View>
  );
}

function App(): React.JSX.Element {
  const isDarkMode = useColorScheme() === 'dark';

  const backgroundStyle = {
    backgroundColor: isDarkMode ? Colors.darker : Colors.lighter,
  };

  useEffect(() => {
    async function init() {
      try {
        await BTClient.setup('sandbox_rzkv7jvj_n6rdmkvv75thbzdw');
      } catch (err) {
        console.log(err);
      }
    }
    init();
  }, []);

  async function handleGooglePay() {
    await BTClient.showGooglePayViewController({
      currencyCode: 'USD',
      requireAddress: true,
      totalPrice: '10.00',
    })
      .then((result: Record<string, string>) => {
        console.log('showGooglePayViewController payload', result);
      })
      .catch((err: any) => console.log(err));
  }

  async function handlePaypal() {
    await BTClient.showPayPalViewController()
      .then((result: Record<string, string>) => {
        console.log('showPayPalViewController payload', result);
      })
      .catch((err: any) => console.log(err));
  }

  async function handleVenmo() {
    await BTClient.showVenmoViewController()
      .then((result: Record<string, string>) => {
        console.log('showVenmoViewController payload', result);
      })
      .catch((err: any) => console.log(err));
  }
  async function handleDataCollector() {
    await BTClient.getDeviceData()
      .then((result: Record<string, string>) =>
        console.log('getDeviceData', result),
      )
      .catch((err: any) => console.log(err));
  }

  return (
    <SafeAreaView style={backgroundStyle}>
      <StatusBar
        barStyle={isDarkMode ? 'light-content' : 'dark-content'}
        backgroundColor={backgroundStyle.backgroundColor}
      />
      <ScrollView
        contentInsetAdjustmentBehavior="automatic"
        style={backgroundStyle}>
        <Header />
        <View
          style={{
            backgroundColor: isDarkMode ? Colors.black : Colors.white,
          }}>
          <Section title="Step One">
            <Button onPress={handleDataCollector} title="Data Collector" />
            <Button onPress={handleGooglePay} title="Google" />
            <Button onPress={handleVenmo} title="Venmo" />
            <Button onPress={handlePaypal} title="PayPal" />
          </Section>
          <Section title="See Your Changes">
            <ReloadInstructions />
          </Section>
          <Section title="Debug">
            <DebugInstructions />
          </Section>
          <Section title="Learn More">
            Read the docs to discover what to do next:
          </Section>
          <LearnMoreLinks />
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  sectionContainer: {
    marginTop: 32,
    paddingHorizontal: 24,
  },
  sectionTitle: {
    fontSize: 24,
    fontWeight: '600',
  },
  sectionDescription: {
    marginTop: 8,
    fontSize: 18,
    fontWeight: '400',
  },
  highlight: {
    fontWeight: '700',
  },
});

export default App;
