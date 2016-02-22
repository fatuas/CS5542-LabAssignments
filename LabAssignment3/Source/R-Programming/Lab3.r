
### K-Means ###

Dataset = read.csv("D:/UMKC/BigData-Lee/Lab3Assignment/Source/Dataset.csv")
x<-rbind(Dataset$Q1_saturation,Dataset$Q2_saturation,Dataset$Q3_saturation,Dataset$Q4_saturation)
x<-t(x)  #this is the transpose function
km <- kmeans(x, 3, 25)
print(km)
plot(x, col = km$cluster)


### K-Medians ###

install.packages("flexclust")
library(flexclust)
imageDF <- Dataset[,-1]
cl2 = kcca(imageDF, k=4, family=kccaFamily("kmedians"), control=list(initcent="kmeanspp"))
image(cl2)
points(imageDF)


### EM ###

install.packages("EMCluster")
library(EMCluster)
imageDF <- Dataset[,-1]
ret.1 <- starts.via.svd(imageDF, nclass = 10, method = "em")
summary(ret.1)
plotem(ret.1, imageDF)


### Hierarchical Clustering ###

d <- dist(as.matrix(Dataset))
hc <- hclust(d)
plot(hc)
