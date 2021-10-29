# PrepaidPhonePlanBalanceChecker
Simple command line tool to check your prepaid phone plan balance.
( Not a beauty regarding SW design, but working ;-) )

This tool can for example be run in a cron job to regularly check your balance and send out an e-mail notification if the balance falls below a given limit.

:information_source: this tool has exclusively been implemented and tested for https://www.aldi-mobile.ch/ and will require adoption for other providers, but may act as an inspiration :)

[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat-square)](http://makeapullrequest.com)\
[![made-with-java](https://img.shields.io/badge/Made%20with-Java-1f425f.svg)](https://www.java.com/)

## How to use?

1. Download the latest release from the [releases](https://github.com/ultimate/PrepaidPhonePlanBalanceChecker/releases):\
[![Latest release](https://badgen.net/github/release/ultimate/PrepaidPhonePlanBalanceChecker)](https://github.com/ultimate/PrepaidPhonePlanBalanceChecker/releases)
3. Create a .properties file for your phone number and provider. Use [example.properties](https://github.com/ultimate/PrepaidPhonePlanBalanceChecker/blob/main/example.properties) as a reference
4. You can run the tool from the console with the following short java command:

```
java -cp bin:lib/activation-1.1.1.jar:lib/javax.mail-1.6.2.jar balancechecker.impl.ALDISuisseBalanceChecker yourname.properties
```

4. Create a script if you want to use execute the program more frequently.\
:information_source: If you want you can output the log to a file, by appending `>> yourname.log` to the script.\
e.g. ```checkbalance.sh```
```
#!/bin/sh
java -cp bin:lib/activation-1.1.1.jar:lib/javax.mail-1.6.2.jar balancechecker.impl.ALDISuisseBalanceChecker yourname.properties >> yourname.log
```

5. Create a cron job to run the script weekly or so\
e.g.
```
0 3 * * 0 cd /path/to/your/copy/of/the/tool && checkbalance.sh
```

## How to adopt?

If you want to use this program for other providers, proceed as follows:

1. Fork this repo\
[![Fork this repo](https://img.shields.io/github/forks/ultimate/PrepaidPhonePlanBalanceChecker.svg?style=social&label=Fork)](https://gitHub.com/ultimate/PrepaidPhonePlanBalanceChecker/fork/)
2. Copy [balancecheker.impl.ALDISuisseBalanceChecker](https://github.com/ultimate/PrepaidPhonePlanBalanceChecker/blob/main/src/balancechecker/impl/ALDISuisseBalanceChecker.java) for your provider
3. Change the following functions for your needs:
   * ```validateProperties0()``` >>> check whether all the settings you need are set in the properties
   * ```getBalance()``` >>> perform the requests to
       1. login to your phone account
       2. retrieve the balance information
       3. parse the balance to java.math.BigDecimal
       4. logout of your account
   * ```main(String[] args)``` >>> change the third line to use your class
   * remove unnecessary constants from the top of the class
4. Create a pull request for this repo if you want this implementation to be shared:\
[![Github pull-requests](https://img.shields.io/github/issues-pr/ultimate/PrepaidPhonePlanBalanceChecker.svg)](https://gitHub.com/ultimate/PrepaidPhonePlanBalanceChecker/pull/)\
**Working on your first Pull Request?** You can learn how from this *free* series [How to make a pull request](https://makeapullrequest.com/)
5. Create a console script and schedule a cron job if needed (see **How to use?** above)

## Improvements needed?

If you have any request... Feel free to create an issue :smile:\
[![GitHub issues](https://img.shields.io/github/issues/ultimate/PrepaidPhonePlanBalanceChecker.svg)](https://GitHub.com/ultimate/PrepaidPhonePlanBalanceChecker/issues/)

## PS / Credits

[![Thanks to Naereen for the badges](https://img.shields.io/badge/Thanks%20to%20Naereen-for%20the%20badges-red.svg)](https://github.com/Naereen/badges)
