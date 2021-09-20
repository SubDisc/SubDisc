# Simple example based on EndToEndTest.java
source("subdisc.R")

testdatafile = "../src/test/resources/adult.txt"


testAdult1 <- .subdisc.single_nominal.cortana_quality(
  src = testdatafile,
  targetColumn = 14,
  targetValue = "gr50K"
)

print(.subdisc.SubgroupSet.tibble(testAdult1))



testAdult2 <- .subdisc.single_numeric.explained_variance(
  src = testdatafile,
  targetColumn = 0,
)

print(.subdisc.SubgroupSet.tibble(testAdult2))


