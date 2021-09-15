library(rJava)
library(tibble)

.jinit("../target/cortana-1.x.x.jar")


# Factory function to load Java enum
.jnewEnum <- function(enumclass){
  function(name){.jfield(enumclass, paste("L", enumclass, ";", sep=""), name)}
}

# Java enum
.subdisc.TargetType             <- .jnewEnum("nl/liacs/subdisc/TargetType")
.subdisc.SearchStrategy         <- .jnewEnum("nl/liacs/subdisc/SearchStrategy")
.subdisc.NumericOperatorSetting <- .jnewEnum("nl/liacs/subdisc/NumericOperatorSetting")
.subdisc.NumericStrategy        <- .jnewEnum("nl/liacs/subdisc/NumericStrategy")
.subdisc.QualityMeasure         <- .jnewEnum("nl/liacs/subdisc/QM")


# General function to call a search

subgroupdiscovery <- function(
  src,
  targetColumn,
  targetValue = NULL,
  targetType = .subdisc.TargetType("SINGLE_NOMINAL"),
  qualityMeasure = .subdisc.QualityMeasure("CORTANA_QUALITY"),
  qualityMeasureMinimum = NULL,
  searchDepth = NULL,
  minimumCoverage = NULL,
  maximumCoverageFraction = NULL,
  maximumSubgroups = NULL,
  maximumTime = NULL,
  searchStrategy = .subdisc.SearchStrategy("BEAM"),
  nominalSets = NULL,
  numericOperatorSetting = .subdisc.NumericOperatorSetting("NORMAL"),
  numericStrategy = .subdisc.NumericStrategy("NUMERIC_BEST"),
  searchStrategyWidth = NULL,
  nrBins = NULL,
  nrThreads = 1
){
  
  # Loading the data table ----
  # Only from file implemented
  if(is.character(src)){
    file <- .jnew("java.io.File", src)
    loader <- .jnew("nl.liacs.subdisc.DataLoaderTXT", file)
    dataTable <- J(loader, "getTable")
    
  } else {
    print("Error: `src` is not valid")
    return(Null)
  }
  
  # Setting the target and target concept ----
  target = .jcall(dataTable, 
                  "Lnl/liacs/subdisc/Column;", 
                  "getColumn", 
                  as.integer(targetColumn))

  targetConcept <- .jnew("nl.liacs.subdisc.TargetConcept")
  
  setTC <- function(func, value, typefunc=identity){
    if(!is.null(value)){ .jcall(targetConcept, "V", func, typefunc(value)) }
  }
  
  setTC( "setPrimaryTarget", target                    )
  setTC( "setTargetType"   , targetType                )
  setTC( "setTargetValue"  , targetValue, as.character )
  
  
  # Setting the search parameters ----
  searchParameters <- .jnew("nl.liacs.subdisc.SearchParameters")
  
  setSP <- function(func, value, typefunc=identity){
    if(!is.null(value)){ .jcall(searchParameters, "V", func, typefunc(value)) }
  }
  
  setSP( "setTargetConcept"          , targetConcept                       )
  setSP( "setQualityMeasure"         , qualityMeasure                      )
  setSP( "setQualityMeasureMinimum"  , qualityMeasureMinimum  , .jfloat    )
  setSP( "setSearchDepth"            , searchDepth            , as.integer )
  setSP( "setMinimumCoverage"        , minimumCoverage        , as.integer )
  setSP( "setMaximumCoverageFraction", maximumCoverageFraction, .jfloat    )
  setSP( "setMaximumSubgroups"       , maximumSubgroups       , as.integer )
  setSP( "setMaximumTime"            , maximumTime            , .jfloat    )
  setSP( "setSearchStrategy"         , searchStrategy                      )
  setSP( "setNominalSets"            , nominalSets                         ) 
  setSP( "setNumericOperators"       , numericOperatorSetting              )
  setSP( "setNumericStrategy"        , numericStrategy                     )
  setSP( "setSearchStrategyWidth"    , searchStrategyWidth    , as.integer )
  setSP( "setNrBins"                 , nrBins                 , as.integer )
  setSP( "setNrThreads"              , nrThreads              , as.integer )
  

  # The actual search call ----
  subgroups <- .jcall(
    obj = "nl.liacs.subdisc.Process", 
    returnSig = "Lnl/liacs/subdisc/SubgroupDiscovery;", 
    method = "runSubgroupDiscovery",
    dataTable,                     # nl.liacs.subdisc.Table theTable
    as.integer(0),                 # int theFold
    .jnull("java.util.BitSet"),    # java.util.BitSet theSelection
    searchParameters,              # nl.liacs.subdisc.SearchParameters theSearchParameters
    FALSE,                         # boolean showWindows
    as.integer(nrThreads),         # int theNrThreads
    .jnull("javax.swing.JFrame")   # javax.swing.JFrame theMainWindow
  )
  
  # Returning the results array
  .jcall(subgroups, "Lnl/liacs/subdisc/SubgroupSet;", "getResult")
}


# Functions to extract the information from the SubgroupSet ----


newGetFunc <- function(getFunc, returnType){
  singleDispatch <- function(obj){ .jcall(obj, returnType, getFunc) }
  function(objVec){ sapply(objVec, singleDispatch) }
} 

.subdisc.getString             <- newGetFunc("toString",              "S")
.subdisc.getCoverage           <- newGetFunc("getCoverage",           "I")
.subdisc.getDepth              <- newGetFunc("getDepth",              "I") 
.subdisc.getFalsePositiveRate  <- newGetFunc("getFalsePositiveRate",  "D")
.subdisc.getID                 <- newGetFunc("getID",                 "I") 
.subdisc.getMeasureValue       <- newGetFunc("getMeasureValue",       "D")
.subdisc.getPValue             <- newGetFunc("getPValue",             "D")
.subdisc.getSecondaryStatistic <- newGetFunc("getSecondaryStatistic", "D") 
.subdisc.getTeriaryStatistic   <- newGetFunc("getTertiaryStatistic",  "D") 
.subdisc.getTruePositiveRate   <- newGetFunc("getTruePositiveRate",   "D") 

#TODO: Handle Java String
#.subdisc.getRegressionModel    <- newGetFunc("RegressionModel",    "S")


# Function to create the tibble (dataframe)
.subdisc.SubgroupSet.tibble <- function(subgroupset){
  
  sglist = as.list(subgroupset)
  tibble(
    Subgroup           = .subdisc.getString(sglist),
    Coverage           = .subdisc.getCoverage(sglist),
    Depth              = .subdisc.getDepth(sglist),
    FalsePositiveRate  = .subdisc.getFalsePositiveRate(sglist),
    ID                 = .subdisc.getID(sglist),
    MeasureValue       = .subdisc.getMeasureValue(sglist),
    PValue             = .subdisc.getPValue(sglist),
    #RegressionModel    = .subdisc.getRegressionModel(sglist) # problem with java string
    SecondaryStatistic = .subdisc.getSecondaryStatistic(sglist),
    TertiaryStatistic  = .subdisc.getTeriaryStatistic(sglist),
    TruePositiveRare   = .subdisc.getTruePositiveRate(sglist)
  )
}

# Helper functions to specific search

.subdisc.single_nominal.cortana_quality <- function(
  src,
  targetColumn,
  targetValue,
  qualityMeasureMinimum = 0.1,
  searchDepth = 1,
  minimumCoverage = 2,
  maximumCoverageFraction = 1.0,
  maximumSubgroups = 1000,
  maximumTime = 1000,
  searchStrategy = .subdisc.SearchStrategy("BEAM"),
  nominalSets = FALSE,
  numericOperatorSetting = .subdisc.NumericOperatorSetting("NORMAL"),
  numericStrategy = .subdisc.NumericStrategy("NUMERIC_BEST"),
  searchStrategyWidth = 10,
  nrBins = 8,
  nrThreads = 1
){
  subgroupdiscovery(
    src = src,
    targetColumn = targetColumn,
    targetValue = targetValue,
    targetType = .subdisc.TargetType("SINGLE_NOMINAL"),
    qualityMeasure = .subdisc.QualityMeasure("CORTANA_QUALITY"),
    qualityMeasureMinimum = qualityMeasureMinimum,
    searchDepth = searchDepth,
    minimumCoverage = minimumCoverage,
    maximumCoverageFraction = maximumCoverageFraction,
    maximumSubgroups = maximumSubgroups,
    maximumTime = maximumTime,
    searchStrategy = searchStrategy,
    nominalSets = nominalSets,
    numericOperatorSetting = numericOperatorSetting,
    numericStrategy = numericStrategy,
    searchStrategyWidth = searchStrategyWidth,
    nrBins = nrBins,
    nrThreads = nrThreads
  )
}

.subdisc.single_numeric.explained_variance <- function(
  src,
  targetColumn,
  qualityMeasureMinimum = 0.1,
  searchDepth = 1,
  minimumCoverage = 2,
  maximumCoverageFraction = 1.0,
  maximumSubgroups = 1000,
  maximumTime = 1000,
  searchStrategy = .subdisc.SearchStrategy("BEAM"),
  nominalSets = FALSE,
  numericOperatorSetting = .subdisc.NumericOperatorSetting("NORMAL"),
  numericStrategy = .subdisc.NumericStrategy("NUMERIC_BEST"),
  searchStrategyWidth = 10,
  nrBins = 8,
  nrThreads = 1
){
  subgroupdiscovery(
    src = src,
    targetColumn = targetColumn,
    targetType = .subdisc.TargetType("SINGLE_NUMERIC"),
    qualityMeasure = .subdisc.QualityMeasure("EXPLAINED_VARIANCE"),
    qualityMeasureMinimum = qualityMeasureMinimum,
    searchDepth = searchDepth,
    minimumCoverage = minimumCoverage,
    maximumCoverageFraction = maximumCoverageFraction,
    maximumSubgroups = maximumSubgroups,
    maximumTime = maximumTime,
    searchStrategy = searchStrategy,
    nominalSets = nominalSets,
    numericOperatorSetting = numericOperatorSetting,
    numericStrategy = numericStrategy,
    searchStrategyWidth = searchStrategyWidth,
    nrBins = nrBins,
    nrThreads = nrThreads
  )
}