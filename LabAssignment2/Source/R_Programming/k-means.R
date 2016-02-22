a = read.table("File.txt")
y <- kmeans(a,3)
y
plot(a[,1], a[,2], col=y$cluster)