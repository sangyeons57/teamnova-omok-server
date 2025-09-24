<?php
// hello.php
header('Content-Type: text/plain; charset=utf-8');
opcache_reset();
echo "hello world \n";
// echo "SAPI=".php_sapi_name()."\n";
// var_dump(getenv('DB_HOST'), getenv('DB_NAME'), getenv('DB_USER'), getenv('DB_PASS'));

