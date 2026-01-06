#install.packages("psych")
#install.packages("dplyr")

library(psych)
library(dplyr)

runWelchTests <- function(data, name, result_frame) {
  # iterate over all numbers of mutations to compare for each with seed ontologies
  for (mutation_count in num_mutations) {
    # select only the two columns that are relevant
    data_filtered <- filter(data, numMutations ==0 | numMutations == mutation_count)
    # perform test
    test_result <- t.test(data_filtered$numFeatures~data_filtered$numMutations, var.equal = FALSE, alternative = "two.sided")
    
    # Extract p-value and confidence interval
    p_value <- test_result$p.value
    ci_lower <- test_result$conf.int[1]
    ci_upper <- test_result$conf.int[2]
    
    # append result to already collected results
    result_frame <- rbind(
      result_frame,
      data.frame(
        Name = paste0(name, "_", mutation_count, "mutations"),
        P_value = p_value,
        CI_95_Lower = -ci_upper, # swap interval limits to have difference between distribution with more mutations and seed
        CI_95_Upper = -ci_lower
      )
    )
  }
  result_frame
}

evaluateBenchmark <- function(input_file, output_file, feature_count) {
  # read raw data for hand-crafted operators
  data_csv <- read.csv(input_file, sep=",")
  
  # normalize to 64 features in total (get proportion that is covered)
  data_csv$numFeatures <- (data_csv$numFeatures)/feature_count
  
  data_1 = filter(data_csv, sampleSize==1)
  data_10 = filter(data_csv, sampleSize==10)
  data_100 = filter(data_csv, sampleSize==100)
  
  # frame to store results
  welch_test_results <- data.frame(
    Name = character(),
    P_Value = numeric(),
    CI_95_Lower = numeric(),
    CI_95_Upper = numeric(),
    stringsAsFactors = FALSE
  )
  
  welch_test_results <- runWelchTests(data_1, "1sample", welch_test_results)
  welch_test_results <- runWelchTests(data_10, "10sample", welch_test_results)
  welch_test_results <- runWelchTests(data_100, "100sample", welch_test_results)
  
  
  
  # export results to csv
  write.csv(welch_test_results, output_file, row.names = FALSE)
}

# possible numbers of mutations
num_mutations <- c(1,2,3,4,5,6,7,8,9,10,15,20,30,40,50,75,100)

# hand-crafted operators
evaluateBenchmark(
  "inputCoverageEL.csv.rawdata.csv",
  "welch_test_results_inputCoverageEL.csv",
  64
)

# extracted operators
evaluateBenchmark(
  "inputCoverageELLearnt.csv.rawdata.csv",
  "welch_test_results_inputCoverageELLearnt.csv",
  64
)

# random operators
evaluateBenchmark(
  "inputCoverageELBaseline.csv.rawdata.csv",
  "welch_test_results_inputCoverageELBaseline.csv",
  64
)



