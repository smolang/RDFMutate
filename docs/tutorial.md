# Tutorial

- a simple tutorial on how to generate a mask for a SUT. This involves specifying different mutation operators and masks.
- we need a sub-folder in our github repository that contains the initial documents.
- based on AUV scenario
- include a command to run the AUV inspection on a custom KG file!
- modify KG: mark start/end of pipeline (special classes)
- given mutation: add pipeline piece at the end
- initial mask: empty --> inspection always works; try different numbers of mutations and look how many pipes are inspected!
- add mutation: move AUV along the pipe --> inspection does not always work!
- add mask: AUV has to be and start or at end of pipe
- edit mutation: add pipe segments everywhere --> inspection does not always work!
- add mask: pipe structure should not branch --> inspection should always work
