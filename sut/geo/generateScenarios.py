import random
import math
import os.path
import sys

def generate():
    
    #overburden rock in two phases
    overburden = [ [random.randint(250, 500), random.randint(50,100), 0] for i in range(2)]
    for i in range(2):
        overburden[i][2] = math.ceil(overburden[i][0]/overburden[i][1])
        overburden[i][0] = overburden[i][1]*overburden[i][2]
     
    ob_layers = ["\tSandstoneUnit ab"+str(i)+" = new SandstoneUnit(null, null, null, null, null, null, "+str(overburden[i][1])+", 2, null);\n\tDepositionGenerator depAb"+str(i)+" = new DepositionGenerator(ab"+str(i)+", 2.0, "+str(overburden[i][2])+");\n\tdl = new List<DepositionGenerator>(depAb"+str(i)+", dl);" for i in range(2)]
     
    #fixed cap size, but may be absent
    cap_layer = ""
    has_cap = (random.randint(1,100) <= 75)
    if has_cap:
      cap_layer = "\tShaleUnit cap = new ShaleUnit(null, null, null, null, null, null, 30.0, 1, 0.0, False, 0);\n\tDepositionGenerator depCap = new DepositionGenerator(cap, 2.0, 1);\n\tdl = new List<DepositionGenerator>(depCap, dl);"
    else:
      cap_layer = "\tSandstoneUnit cap = new SandstoneUnit(null, null, null, null, null, null, 30.0, 2, null);\n\tDepositionGenerator depCap = new DepositionGenerator(cap, 2.0, 1);\n\tdl = new List<DepositionGenerator>(depCap, dl);"
    
    
    #fixed reservoir, ignore
    reservoir_layer = "\tSandstoneUnit ekofisk = new ChalkUnit(null, null, null, null, null, null, 99.0, 2, null);\n\tDepositionGenerator depEko = new DepositionGenerator(ekofisk, 2.0, 1);\n\tdl = new List<DepositionGenerator>(depEko, dl);"
    
    #filling layers
    fill = [ [random.randint(50, 400), random.randint(20,50),0] for i in range(4)]
    for i in range(4):
        fill[i][2] = math.ceil(fill[i][0]/fill[i][1])
        fill[i][0] = fill[i][0]*fill[i][2]
     
    fill_layers = ["\tSandstoneUnit fill"+str(i)+" = new SandstoneUnit(null, null, null, null, null, null, "+str(fill[i][1])+", 2, null);\n\tDepositionGenerator fillAb"+str(i)+" = new DepositionGenerator(fill"+str(i)+", 2.0, "+str(fill[i][2])+");\n\t dl = new List<DepositionGenerator>(fill"+str(i)+", dl);" for i in range(4)]
     
    #fixed source, just boolean
    has_source = (random.randint(1,100) <= 90) 
    source_layer = "\tShaleUnit source = new ShaleUnit(null, null, null, null, null, null, 40.0, 1, 0.0, "+("True" if has_source else "False")+", 0);"
    
    
    steps = 3 + sum([ fill[i][2] for i in range(4)])+ sum([ overburden[i][2] for i in range(2)])
    depth = 40+30+99 + sum([ fill[i][0] for i in range(4)])+ sum([ overburden[i][0] for i in range(2)])
    
    finish = "\tdl = dl.reverse();\n\tDriver driver = new Driver(null,null);\n\tdriver.sim(dl, "+str(2*steps)+".0, source, 0.0);\n\n\n/*\n has_source: "+str(has_source)+"\nhas_cap: "+str(has_cap)+"\ndepth: "+str(depth)+"\n*/\nend"
    
    return "main\n"+source_layer+"\tList<DepositionGenerator> dl = null;\n\n"+fill_layers[0]+"\n"+fill_layers[1]+"\n"+fill_layers[2]+"\n"+fill_layers[3]+"\n"+reservoir_layer+"\n"+cap_layer+"\n"+ob_layers[0]+"\n"+ob_layers[1]+"\n"+finish
    
    
dir = sys.argv[1]
if not os.path.exists(dir):
    print("Directory "+dir+"dots not exist!")
    exit

nr = sys.argv[2]
if not nr.isdigit():
    print("Parameter "+nr+"is not a number!")
    exit
    

for i in range(int(nr)):
    with open(os.path.join(dir,"Output"+str(i)+".txt"), "w") as text_file:
        text_file.write(generate())
