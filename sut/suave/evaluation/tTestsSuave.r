#install.packages("psych")
#install.packages("dplyr")

library(psych)
library(dplyr)

runTTests <- function(data, name, result_frame) {
  # iterate over all numbers of mutations to compare for each with seed ontologies
  for (mutation_count in num_mutations) {
    # select only the two columns that are relevant
    data_filtered <- filter(data, numMutations == mutation_count)
    # perform one-sample, one-sided t-test
    
    test_result <- tryCatch({
      t.test(data_filtered$numFeatures,mu=1,alternative = "less")
    }, error = function(e) {
      # error occurred in T-test
      NA
    })
  
    # check, if test was successful
    if (length(test_result) ==1 && is.na(test_result)) {
      p_value <- NA
      ci_lower <- NA
      ci_upper <- NA
    }
    else {
      # Extract p-value and confidence interval
      p_value <- test_result$p.value
      ci_lower <- test_result$conf.int[1]
      ci_upper <- test_result$conf.int[2]
    }
    
    
    
    # append result to already collected results
    result_frame <- rbind(
      result_frame,
      data.frame(
        Name = paste0(name, "_", mutation_count, "mutations"),
        P_value = p_value,
        CI_95_Lower = ci_lower,
        CI_95_Upper = ci_upper
      )
    )
  }
  result_frame
}

evaluateBenchmark <- function(input_file, output_file, feature_count) {
  
  
  # read raw data for hand-crafted operators
  data_csv <- read.csv(input_file, sep=",")
  
  # normalize to number of features in total (get proportion that is covered)
  data_csv$numFeatures <- (data_csv$numFeatures)/feature_count
  
  data_1 = filter(data_csv, sampleSize==1)
  data_10 = filter(data_csv, sampleSize==10)
  data_100 = filter(data_csv, sampleSize==100)
  
  # frame to store results
  t_test_results <- data.frame(
    Name = character(),
    P_Value = numeric(),
    CI_95_Lower = numeric(),
    CI_95_Upper = numeric(),
    stringsAsFactors = FALSE
  )
  
  t_test_results <- runTTests(data_1, "1sample", t_test_results)
  
  # only for 75 and 100, the values are not contant
  t_test_results <- runTTests(data_10, "10sample", t_test_results)
  
  # always constant
  t_test_results <- runTTests(data_100, "100sample", t_test_results)
  
  
  
  # export results to csv
  write.csv(t_test_results, output_file, row.names = FALSE)
}

# possible numbers of mutations
num_mutations <- c(1,2,3,4,5,6,7,8,9,10,15,20,30,40,50,75,100)

# hand-crafted operators
evaluateBenchmark(
  "inputCoverageSuave.csv.rawdata.csv",
  "t_test_results_inputCoverageSuave.csv",
  88
)

# random operators
evaluateBenchmark(
  "inputCoverageSuaveBaseline.csv.rawdata.csv",
  "t_test_results_inputCoverageSuaveBaseline.csv",
  88
)


