import sched, time, hashlib, json, sys, time
import Adafruit_DHT as dht

import urllib
import urllib.request
from bs4 import BeautifulSoup


class Block:
    hashVal = ""
    prevHash = ""
    data = 0.0
    timeStamp = ""

    def __init__(self, data, prevHash):
        self.data = data
        self.prevHash = prevHash
        self.timeStamp = time.asctime(time.localtime(time.time()))
        self.hashVal = self.calculatedHash()

    def applySha256(self, input):

        if type(input) != str:
            input = son.dumps(input, sort_keys=True)  # sorting keys to guarantee non-	repeatable hashes

        if sys.version_info.major == 2:
            return "error in if"
        else:
            input = json.dumps(input, sort_keys=True).encode()
            return hashlib.sha256(input).hexdigest()

    def calculatedHash(self):
        dataAsString = str(self.data)
        calcHashVal = self.applySha256(self.hashVal + dataAsString + self.timeStamp)
        return calcHashVal

    def printBlock(self):  # self is the positional argument
        print("{ \n    prevHash:    ", self.prevHash, "\n    hash:        ", self.hashVal, "\n    data:        ",
              self.data, " degrees", "\n    timeStamp:   ", self.timeStamp, "\n}")  # access class attribubtes with self

    def getHash(self):
        return self.hashVal

    def getPrevHash(self):
        return self.prevHash

    def getData(self):
        return self.data

    def getTimeStamp(self):
        return self.timeStamp

    def setData(self, data):
        self.data = data


class Driver():
    # block1 = Block(5.0, "10101")
    # block1.printBlock()

    # out = open("dataReceived.txt", "w+")

    # this is weather operation
    # arduinoTemp = []
    # websiteTemp = []
    # webValid = []
    # trueChain = []
    # webChain = []

    # s = sched.scheduler(time.time, time.sleep)
    # counter2 = 0

    def __init__(self):
        self.out = open("dataReceived.txt", "w+")  # all on desktop of pi
        self.webValid = [False, False, False, False, False, False, False, False, False, False, False, False]
        self.trueChain = []
        self.webChain = []
        self.howFarFromTrue = [-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1]

        self.s = sched.scheduler(time.time, time.sleep)
        self.counter2 = 0

    def actualOperation(self):
        print("Runnning.... ", time.asctime(time.localtime(time.time())))

        while (self.counter2 < 12):
            if self.counter2 == 0:

                h, t = dht.read_retry(dht.DHT22, 4)
                f = round((9.0 / 5.0 * t + 32), 2)

                url = "https://www.wunderground.com/hourly/us/ga/athens/date/2018-04-17?cm_ven=localwx_hour"
                page = urllib.request.urlopen(url)
                soup = BeautifulSoup(page, "html.parser")
                body = soup.find('tbody').findNext('tr').findNext('td').findNext('td').findNext('td')
                sequence = (body.text[2], body.text[3])
                webTemp = "".join(sequence)
                webValue = float(webTemp)

                self.trueChain.append(Block(f, "0"))
                self.webChain.append(Block(webValue, "0"))
                print(webValue,
                      " This value is extracted during each iteration.  But since period so small, there will be small change in web-source predicted outside temp.")
                print("User boundaries: +/- 6.0 degrees F")

                if ((self.webChain[self.counter2].getData() == self.trueChain[self.counter2].getData()) or (
                        (self.webChain[self.counter2].getData() > self.trueChain[self.counter2].getData()) and (
                        self.webChain[self.counter2].getData() <= (self.trueChain[self.counter2].getData() + 6))) or (
                        (self.webChain[self.counter2].getData() < self.trueChain[self.counter2].getData()) and (
                        self.webChain[self.counter2].getData() >= (self.trueChain[self.counter2].getData() - 6)))):
                    self.trueChain[self.counter2].setData(webValue)
                    self.webValid[self.counter2] = True
                    print("web-source within bounds - if")
                # ends inner if
                self.howFarFromTrue[self.counter2] = abs(round((webValue - f),
                                                               2))  # f is also self.trueChain[self.counter2].getData().  Latter used in conditionals to show proof of blockchain comparison understanding
            # ends outer if
            else:
                h, t = dht.read_retry(dht.DHT22, 4)
                f = round((9.0 / 5.0 * t + 32), 2)

                url = "https://www.wunderground.com/hourly/us/ga/athens/date/2018-04-17?cm_ven=localwx_hour"
                page = urllib.request.urlopen(url)
                soup = BeautifulSoup(page, "html.parser")
                body = soup.find('tbody').findNext('tr').findNext('td').findNext('td').findNext('td')
                sequence = (body.text[2], body.text[3])
                webTemp = "".join(sequence)
                webValue = float(webTemp)
                print(webValue, " Web source temp")

                self.trueChain.append(Block(f, self.trueChain[len(self.trueChain) - 1].getHash()))
                self.webChain.append(Block(webValue, self.trueChain[len(self.trueChain) - 1].getHash()))
                if ((self.webChain[self.counter2].getData() == self.trueChain[self.counter2].getData()) or (
                        (self.webChain[self.counter2].getData() > self.trueChain[self.counter2].getData()) and (
                        self.webChain[self.counter2].getData() <= (self.trueChain[self.counter2].getData() + 6))) or (
                        (self.webChain[self.counter2].getData() < self.trueChain[self.counter2].getData()) and (
                        self.webChain[self.counter2].getData() >= (self.trueChain[self.counter2].getData() - 6)))):
                    self.trueChain[self.counter2].setData(webValue)
                    self.webValid[self.counter2] = True
                    print("web-source within bounds - else")

                self.howFarFromTrue[self.counter2] = abs(round((webValue - f), 2))
            self.trueChain[self.counter2].printBlock()

            try:
                self.out.write(self.trueChain[self.counter2].getPrevHash())
                self.out.write("\n")
                self.out.write(self.trueChain[self.counter2].getHash())
                self.out.write("\n")
                self.out.write(str(self.trueChain[self.counter2].getData()))
                self.out.write("\n")
                self.out.write(self.trueChain[self.counter2].getTimeStamp())
                self.out.write("\n")


            except IOError:
                print("An error occurred writing to file")

            self.counter2 += 1

            if self.counter2 != 11:
                time.sleep(10)
            if self.counter2 == 12:
                try:
                    distances = str(self.howFarFromTrue)
                    self.out.write(distances)
                    self.out.close()
                    print("Blockchain successfull!")
                except IOError:
                    print("An error occurred closing file")
                i = 0
                print("User boundaries: +/- 6 degrees F")
                while (i < self.counter2):
                    print("WebSourceValid [", i, "]", self.webValid[i])
                    print("HowFarFromTrue [", i, "]", self.howFarFromTrue[i])
                    i += 1


class Tester():
    driverObj = Driver()
    driverObj.actualOperation()
