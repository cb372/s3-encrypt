# s3-encrypt

A command line tool for uploading and downloading encrypted files to S3.

Files are encrypted using [client-side encryption with a KMS-managed customer master key](http://docs.aws.amazon.com/AmazonS3/latest/dev/UsingClientSideEncryption.html) before being uploaded.

Note: This tool will become obsolete if/when https://github.com/aws/aws-cli/issues/1686 gets fixed.

## How to install

1. Clone this repo

2. (Optional) add `bin/s3-encrypt` to your path.

## How to use

```
Usage: s3-encrypt [upload|download] [options] s3-url local-file kms-key-id

  -r, --region <value>   AWS region
  -p, --profile <value>  AWS credentials profile
Command: upload
Encrypt and upload a file to S3
Command: download
Download a file from S3 and decrypt it
  s3-url                 S3 URL e.g. s3://my-bucket/my-file.txt
  local-file             Local file path. Use "stdin" to upload from stdin, "stdout" to download to stdout
  kms-key-id             KMS master key ID
```

### To upload a file

```
$ s3-encrypt upload -p my-aws-profile s3://my-bucket/my-secret-file.txt local-file.txt 123456-abcd-abcd-1234-abcdabcd
```

To upload text from stdin:

```
$ echo "here's the password" | s3-encrypt upload -p my-aws-profile s3://my-bucket/my-secret-file.txt stdin 123456-abcd-abcd-1234-abcdabcd
```

### To download a file

```
$ s3-encrypt download -p my-aws-profile s3://my-bucket/my-secret-file.txt local-file.txt 123456-abcd-abcd-1234-abcdabcd
```

To download the file's content and print it to stdout:

```
$ s3-encrypt download -p my-aws-profile s3://my-bucket/my-secret-file.txt stdout 123456-abcd-abcd-1234-abcdabcd
```
