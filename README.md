# SMS Link

SMS Link is a tool to extract SMS links from SMS Backup Android Application.

## Get Help

```
./mill smslinks.run
```

## Run App

Download your smsbackup locally.

Run the command:
```
./mill smslinks.run xml --phone 1234567890 data/sms-sample.xml 
```

## Performance

To process the data/sms-sample.xml file:
```
real    0m3.648s
user    0m8.150s
sys     0m0.541s
```

To process a backup of 132MB generating ~ 250 unique links:
```
real    0m20.375s
user    0m26.638s
sys     0m11.822s
```
