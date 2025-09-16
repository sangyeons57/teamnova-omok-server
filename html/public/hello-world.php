<?php
// hello.php
header('Content-Type: text/plain; charset=utf-8');
echo "hello world";
echo "SAPI=".php_sapi_name()."\n";
var_dump(getenv('DB_HOST'), getenv('DB_NAME'), getenv('DB_USER'), getenv('DB_PASS'));

