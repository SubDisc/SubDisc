# How to use git from the command line

## Init - initiate git on a folder  
```
$ git init  
```

## Set up an SSH conection to GitHub
https://docs.github.com/en/github/authenticating-to-github/connecting-to-github-with-ssh

## Clone - get a remote git repository copy locally
```
$ git clone "url github"  
```

## Status - get a list of files which have been altered 
```
$ git status
```

## Show branch list  
```
$ git branch
```

## Checkout branch 
```
$ git checkout "branchname"
```

## Make new branch 
```
$ git branch "branchname"
```

## Add files 
```
$ git add "filename"
```

## Commit added files  
```
$ git commit -m "message text"
```

## Push commit to remote 
```
$ git push origin "branchname"
```

## Pull commits from remote  
```
$ git pull origin "branchname"
```

## Merge branch to main  
```
$ git checkout main 
$ git pull origin main
$ git merge "branchname"
$ git push origin main  
```

## Merge main to branch  
```
$ git checkout "branchname" 
$ git pull origin "branchname"  
$ git merge main  
$ git push origin "branchname"
```

## Delete branch 
```
$ git branch -d "branchname"
```

## Show merge branch tree  
```
$ gitk
```

# Install Git Bash

https://gitforwindows.org/

It will give a nice command prompt for Linux commands (emulator for Linux on Windows).

![image](https://user-images.githubusercontent.com/37830964/116661126-4272b600-a994-11eb-8c54-94e9c7f1d0ef.png)

![image](https://user-images.githubusercontent.com/37830964/116661403-af864b80-a994-11eb-87e0-e07b95e1627f.png)


